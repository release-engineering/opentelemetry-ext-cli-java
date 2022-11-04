# Introduction

This is a specialized Opentelemetry trace context propagator for command-line applications. It's is especially useful when your CLI tools are used from Jenkins pipelines with the Jenkins Opentelemetry Plugin enabled. This plugin exposes specific environment variables to tool executions:

* `TRACEPARENT`
* `TRACESTATE`
* `TRACE_ID`
* `SPAN_ID`

If your CLI tools call other services, it can be very important to consume this context so you can propagate the trace to those other services.

## Example: Command-Line Interface Usage

The `OtelCLIHelper` utility class is intended to move the Opentelemetry setup out of your way:

```java
public static void main(String[] args)
{
    OtelCLIHelper.startOtel(
        "my-service", 
        OtelCLIHelper.defaultSpanProcessor(
            OtelCliHelper.defaultSpanExporter("http://localhost:4317")
        )
    );

    try
    {
        Span.current().setAttribute("input-file", args[0]);
        // do some cool stuff
    }
    finally
    {
        OtelCLIHelper.stopOtel();
    }
}
```

If you'd like more control over the setup, you can handle it manually like this:

```java
Resource resource = Resource.getDefault()
                            .merge( Resource.create( Attributes.of( ResourceAttributes.SERVICE_NAME,
                                                                    serviceName ) ) );

SpanExporter exporter = OtlpGrpcSpanExporter.builder().setEndpoint( endpoint ).build();
SpanProcessor processor = BatchSpanProcessor.builder(exporter).build();
SdkTracerProvider sdkTracerProvider =
                SdkTracerProvider.builder().addSpanProcessor( processor ).setResource( resource ).build();

// NOTE the use of EnvarExtractingPropagator here
OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                                                 .setTracerProvider( sdkTracerProvider )
                                                 .setPropagators( ContextPropagators.create(
                                                                 EnvarExtractingPropagator.getInstance() ) )
                                                 .buildAndRegisterGlobal();

Context parentContext = EnvarExtractingPropagator.getInstance().extract( Context.root(), null, null );
Span root = GlobalOpenTelemetry.get()
                               .getTracer( serviceName )
                               .spanBuilder( "cli-execution" )
                               .setParent( parentContext )
                               .startSpan();
root.makeCurrent();
```

## Example: Quarkus Setup

Quarkus usage may seem a little weird for a context propagator that reads trace context from environment variables. It doesn't match the typical request-driven context propagation. However, for certain use cases we use a Quarkus as a Kubernetes sidecar, and the main container is a single execution associated with a larger trace. In this very specific use case, we actually want to pull the trace context from the environment variables and IGNORE those coming from the main container.

At least for now. Maybe this is a terrible idea, and we'll erase this use case and quietly back away... 

To enable this propagator in Quarkus, add the following to your `application.yaml` file:

```yaml
quarkus:
  [...]

  opentelemetry:
    enabled: true
    propagators:
      - envar
```

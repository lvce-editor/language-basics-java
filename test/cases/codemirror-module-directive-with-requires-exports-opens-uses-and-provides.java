module com.example.foo {
    requires com.example.http;
    requires java.logging;

    requires transitive com.example.network;

    exports com.example.bar;
    exports com.example.internal to com.example.probe;

    opens com.example.quux;
    opens com.example.internal to com.example.network, com.example.probe;

    uses com.example.Intf;
    provides com.example.Intf with com.example.Impl;
}

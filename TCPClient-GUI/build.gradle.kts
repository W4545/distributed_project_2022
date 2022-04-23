plugins {
    java
    application
}

dependencies {
    implementation(project(":TCPClient"))
}

application {
    mainClass.set("GUI")
}
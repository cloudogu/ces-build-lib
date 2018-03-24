package com.cloudogu.ces.cesbuildlib

@Grab('junit:junit:4.12')
import org.junit.runner.*

class JUnit implements Serializable {

    JUnit(script) {
        this.script = script
    }


    Result runClasses(Class... classes) {
        JUnitCore.runClasses(classes)
    }
}
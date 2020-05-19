package com.cloudogu.ces.cesbuildlib

class Changelog {
    private String name

    Changelog(String name) {
        this.name = name
    }

    String get() {
        return new File(name).getText('UTF-8')
    }
}

package com.cloudogu.ces.cesbuildlib

def call() {
    return new Build(this).isNightly()
}


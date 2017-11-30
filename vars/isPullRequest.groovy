package com.cloudogu.ces.cesbuildlib

def call() {
    // CHANGE_ID == pull request id
    // http://stackoverflow.com/questions/41695530/how-to-get-pull-request-id-from-jenkins-pipeline
    env.CHANGE_ID != null && env.CHANGE_ID.length() > 0
}
package com.cloudogu.ces.cesbuildlib

def call() {
    String regexMatchesHostName = 'https?://([^:/]*)'

    // Storing matcher in a variable might lead to java.io.NotSerializableException: java.util.regex.Matcher
    if (!(env.JENKINS_URL =~ regexMatchesHostName)) {
        error 'Unable to determine hostname from env.JENKINS_URL. Expecting http(s)://server:port/jenkins'
    }
    return (env.JENKINS_URL =~ regexMatchesHostName)[0][1]
}
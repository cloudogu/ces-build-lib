package com.cloudogu.ces.cesbuildlib

class ChangelogMock extends Changelog{
    private content;

    ChangelogMock(String content) {
        super("", null)
        this.content = content
    }

    String get(){
        return this.content
    }
}

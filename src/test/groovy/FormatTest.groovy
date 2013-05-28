package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class FormatTest extends PMSEncoderTestCase {
    void testMatch() {
        def web = new WEB()
        assert web.match('http://example.com')
        assert web.match('mms://example.com')
        assert !web.match('tv://example.com')
        assert !web.match('about://example.com')
        assert !web.match('about://example.com/example.http')
        assert !web.match('about://example.com/example.mms')
        assert !web.match('about://example.com/example.tv')
        assert !web.match('/example.com')
        assert !web.match('/example.com/example.http')
        assert !web.match('/example.com/example.mms')
        assert !web.match('/example.com/example.tv')
    }
}

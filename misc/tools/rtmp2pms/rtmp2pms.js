var command = prompt(
    'Please enter the rtmpdump command line',
    'rtmpdump -v -r rtmp://example.org -s http://example.org -p http://example.org'
);

if (command) {
    command = command.replace(/^.*?rtmpdump(?:\.exe)?\s+/i, '').
        replace(/\s+$/, '').
        replace(/["']/g, '').
        replace(/(--?[a-zA-Z]+)\s+(?!-)(?:(?:"([^"]+)")|(?:'([^']+)')|(\S+))/g, function(match, key, v1, v2, v3) { return key + '=' + escape(v1 || v2 || v3) }).
        replace(/\s+/g, '&');
    document.write('rtmpdump://rtmp2pmsjs?' + command);
}

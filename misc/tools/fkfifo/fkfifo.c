/*
 * chocolateboy 2011-02-27
 * inspired by/based on: http://forum.doom9.org/showthread.php?s=8b5f518ea5d237044f2728a1943712d5&p=1226337#post1226337
 * thanks to roozhou
 */

#include <stdio.h>
#include <stdlib.h>
#include <windows.h>

#define BUFSIZE 0x400000 // 4MB
#define ARGS_LENGTH 512
#define USAGE 1
#define BAD_PIPE 2
#define BAD_ARGS 3

static char Args[ARGS_LENGTH] = { 0 };

int usage(char* executable) {
    fprintf(stderr, "usage: %s \\\\.pipe cmd [ arg1, ... ]\r\n", executable);
    return USAGE;
}

int append(size_t offset, char *str, size_t len) {
    if ((offset + len) >= ARGS_LENGTH) {
        fprintf(stderr, "args length exceeds buffer size\r\n");
        exit(BAD_ARGS);
    } else {
        strncat(Args + offset, str, len);
    }

    return offset + len;
}

int main(int argc, char **argv) {
    char *pipe, *cmd;
    HANDLE hPipe;
    STARTUPINFOA stinfo = { 0 };
    PROCESS_INFORMATION pinfo = { 0 };
    SECURITY_ATTRIBUTES sa = { sizeof(SECURITY_ATTRIBUTES), NULL, TRUE };

    if (argc < 3) {
        return usage(argv[0]);
    }

    pipe = argv[1];
    cmd = argv[2];

    size_t i, j = 0;

    for (i = 3; i < argc; ++i) {
        if (i > 3)
            j = append(j, " ", 1);
        j = append(j, argv[i], strlen(argv[i]));
    }

    fprintf(stderr, "PIPE: \"%s\"\r\n", pipe);
    fprintf(stderr, "CMD: \"%s\"\r\n", cmd);
    fprintf(stderr, "ARGS: \"%s\"\r\n", Args);

    hPipe = CreateNamedPipeA(
        pipe,
        PIPE_ACCESS_DUPLEX,
        PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT,
        PIPE_UNLIMITED_INSTANCES,
        BUFSIZE,
        BUFSIZE,
        0,
        &sa
    );

    if (hPipe == INVALID_HANDLE_VALUE)
        return BAD_PIPE;

    ConnectNamedPipe(hPipe, NULL);
    stinfo.cb = sizeof(STARTUPINFO);
    stinfo.dwFlags = STARTF_USESTDHANDLES;
    stinfo.hStdInput = hPipe; // redirect
    stinfo.hStdOutput = GetStdHandle(STD_OUTPUT_HANDLE); // inherit
    stinfo.hStdError = GetStdHandle(STD_ERROR_HANDLE); // inherit

    if (CreateProcessA(cmd, Args, 0, 0, TRUE, 0, NULL, NULL, &stinfo, &pinfo)) {
        WaitForSingleObject(pinfo.hProcess, INFINITE);
    }

    return 0;
}

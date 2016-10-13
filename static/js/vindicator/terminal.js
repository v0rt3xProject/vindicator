var term;
var command = "";

var terminalContainer = document.getElementById('terminal-container');

function getUpdates() {

}

function setUpdates(data) {
    if (data.hasOwnProperty(data.view)) {
        var array = (data[data.view]);
        for (var i = 0; i < array.length; i++) {
            term.write(JSON.stringify(array[i])+"\r\n");
        }
    } else {
        term.write(JSON.stringify(data));
    }

    term.prompt();
}

function createTerminal() {
    while (terminalContainer.children.length) {
        terminalContainer.removeChild(terminalContainer.children[0]);
    }

    term = new Terminal({
        cursorBlink: true,
    });

    term.open(terminalContainer);
    term.fit();

    runTerminal();
}

function runTerminal() {
    term._initialized = true;

    var shellprompt = '$ ';

    term.prompt = function () {
        term.write('\r\n' + shellprompt);
    };

    term.writeln('Welcome Vindicator Shell');
    term.writeln('');
    term.prompt();

    term.on('key', function (key, ev) {
        var printable = (
            !ev.altKey && !ev.altGraphKey && !ev.ctrlKey && !ev.metaKey
        );

        if (ev.keyCode == 13) {
            send({
                view: command.split(" ")[0],
                action: command.split(" ")[1]
                // view: "terminal",
                // action: "execute",
                // cmd: command.split(" ")[0],
                // args: command.split(" ").slice(1)
            });
            command = "";
            term.write("\r\n");
        } else if (ev.keyCode == 8) {
            if (term.x > 2) {
                command = command.slice(0, command.length - 2);
                term.write('\b \b');
            }
        } else if (printable) {
            command += key;
            term.write(key);
        }
    });

    term.on('paste', function (data, ev) {
        term.write(data);
    });
}

createTerminal();
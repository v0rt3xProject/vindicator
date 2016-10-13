function getUpdates() {

}

function setUpdates(data) {

}

$('#themis_save').click(function () {
    var themis_protocol = $('#themis_protocol').val();
    var themis_host = $('#themis_host').val();
    var themis_port = $('#themis_port').val();

    if (themis_host.length == 0) {
        notify("danger", "Themis Host shouldn't be empty");
        return;
    }

    if (themis_port.length == 0) {
        notify("danger", "Themis Port shouldn't be empty");
        return;
    }

    send({
        view: "config",
        action: "set",
        configType: "themis",
        configValue: {
            themis: {
                protocol: themis_protocol,
                host: themis_host,
                port: themis_port
            }
        }
    })
});

$('#xploit_save').click(function () {
    var xploit_team = $('#xploit_team').val();
    var xploit_interval = $('#xploit_interval').val();

    if (xploit_team.length == 0) {
        notify("danger", "eXploit: Team shouldn't be empty");
        return;
    }

    if (xploit_interval.length == 0) {
        notify("danger", "eXploit: Interval shouldn't be empty");
        return;
    }

    send({
        view: "config",
        action: "set",
        configType: "xploit",
        configValue: {
            xploit: {
                team: xploit_team,
                interval: xploit_interval
            }
        }
    })
});
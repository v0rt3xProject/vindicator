function getUpdates() {
    send({
        view: "index",
        action: "overview"
    });
}

function setUpdates(data) {
    if (data.view == "index") {
        if (data.type == "overview") {
            var stats = data['stats'];
            var queued = stats['queued'];

            $("#stat_hi").text(queued['high']);
            $("#stat_no").text(queued['normal']);
            $("#stat_lo").text(queued['low']);
            $("#stat_que_tot").text(queued['low'] + queued['normal'] + queued['high']);

            $("#stat_processing").text(stats['processing']);
            $("#stat_accepted").text(stats['sent']);
            $("#stat_rejected").text(stats['invalid']);

            $("#exploits").text(data['exploits']);
            $("#remote_agents").text(data['remote_agents']);
            $("#services").text(data['services']);
            $("#teams").text(data['teams']);
        }
    }
}
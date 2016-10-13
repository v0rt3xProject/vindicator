function getUpdates() {
    send({
        view: "flag",
        action: "list"
    })
}

function setUpdates(data) {
    if (data.view == "flag") {
        if (data.type == "list") {
            var stats = data['stats'];
            var queued = stats['queued'];

            $("#stat_hi").text(queued['high']);
            $("#stat_no").text(queued['normal']);
            $("#stat_lo").text(queued['low']);
            $("#stat_que_tot").text(queued['low'] + queued['normal'] + queued['high']);

            $("#stat_processing").text(stats['processing']);
            $("#stat_accepted").text(stats['sent']);
            $("#stat_rejected").text(stats['invalid']);
            $("#last_updated").text(data['timestamp']);

            var id = $("#stat_flags tr").first().attr("id");

            var flags = data['flags'];

            for (var j = flags.length - 1; j >= 0; j--) {
                if ($("#" + flags[j]['id'] + "_" + flags[j]['state']).length == 0) {
                    var row = $("<tr></tr>").attr("id", flags[j]['id'] + "_" + flags[j]['state']);
                    var state_class = "text-primary";

                    switch (flags[j]['state']) {
                        case "Queued":
                            state_class = "text-warning";
                            break;
                        case "Accepted":
                            state_class = "text-success";
                            break;
                        case "Rejected":
                            state_class = "text-danger";
                            break;
                    }

                    row.append($("<td></td>").text(flags[j]['id']));
                    row.append($("<td></td>").attr("class", "text-muted").text(flags[j]['flag']));
                    row.append($("<td></td>").text(flags[j]['priority']));
                    row.append($("<td></td>").attr("class", state_class).text(flags[j]['state']));
                    row.append($("<td></td>").text(flags[j]['time']));

                    console.log(row);
                    $("#stat_flags").prepend(row);
                }
            }
        }
    }
}
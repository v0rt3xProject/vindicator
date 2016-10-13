function getUpdates() {
    send({
        view: "team",
        action: "list"
    })
}

function setUpdates(data) {
    if (data.view == "team") {
        if (data.type == "add") {
            if (data.success) {
                $("#new_team_id").val("");
                $("#new_team_name").val("");
                $("#new_team_ip").val("");
            }
        } else if (data.type == "list") {
            for (var i = 0; i < data.teams.length; i++) {
                var team = data.teams[i];
                if (!$("tr#" + team.name).length) {
                    var team_row = $("<tr></tr>").attr("id", team.name);

                    team_row.append($("<td></td>").text(team.id));
                    team_row.append($("<td></td>").text(team.name));
                    team_row.append($("<td></td>").text(team.ip));

                    $("#team_list").append(team_row);
                }
            }
        }
    }
}

$("#new_team").click(function () {
    var team_id = $("#new_team_id").val();
    var team_name = $("#new_team_name").val();
    var team_ip = $("#new_team_ip").val();

    if (team_id.length == 0) {
        notify("danger", "TeamID shouldn't be empty!");
        return;
    }

    if (team_name.length == 0) {
        notify("danger", "Team name shouldn't be empty!");
        return;
    }

    if (team_ip.length == 0) {
        notify("danger", "Team IP shouldn't be empty!");
        return;
    }

    send({
        view: "team",
        action: "add",
        team: {
            id: team_id,
            name: team_name,
            ip: team_ip
        }
    });
});
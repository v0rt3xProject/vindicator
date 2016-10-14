function getUpdates() {
    send({
        view: "agent",
        action: "list"
    })
}

function setUpdates(data) {
    if (data.view == "agent") {
        if (data.type == "list") {
            for (var i = 0; i < data.agents.length; i++) {
                var agent = data.agents[i];
                if (!$("tr#" + agent.id).length) {
                    var agent_row = $("<tr></tr>").attr("id", agent.id);

                    agent_row.append($("<td></td>").text(agent.id));
                    agent_row.append($("<td></td>").text(agent.type));
                    agent_row.append($("<td></td>").text(agent.hostName));
                    agent_row.append($("<td></td>").text(agent.osName));
                    agent_row.append($("<td></td>").text(agent.osArch));
                    if (data.timestamp - agent.lastSeen < 20000) {
                        agent_row.append(
                            $("<td></td>")
                                .attr("id", "status_" + agent.id)
                                .attr("class", "text-success")
                                .text("On-Line")
                        );
                    } else {
                        agent_row.append(
                            $("<td></td>")
                                .attr("id", "status_" + agent.id)
                                .attr("class", "text-danger")
                                .text("Off-Line")
                        );
                    }
                    agent_row.append($("<td></td>").attr("id", "task_" + agent.id).text((agent.task) ? agent.task : "-"));
                    agent_row.append($("<td></td>").attr("id", "state_" + agent.id).text(agent.taskState));

                    agent_row.append($("<td></td>").append(
                        $("<button></button>")
                            .attr('type', 'button')
                            .attr('class', 'btn btn-danger btn-sm')
                            .click(function () {
                                deleteAgent(agent.id);
                            })
                            .text("Delete")
                    ));

                    $("agent_list").append(agent_row);
                } else {
                    if (data.timestamp - agent.lastSeen < 20000) {
                        $("td#status_" + agent.id).attr("class", "text-success").text("On-Line");
                    } else {
                        $("td#status_" + agent.id).attr("class", "text-danger").text("Off-Line");
                    }
                    $("td#task_" + agent.id).text((agent.task) ? agent.task : "-");
                    $("td#state_" + agent.id).text(agent.taskState);
                }
            }
        } else if (data.type == "delete") {
            if (data.success) {
                $("tr#" + data.target).remove();
            }
        }
    }
}

function deleteAgent(agentId) {
    send({
        action: 'delete',
        view: 'agent',
        target: agentId
    });
}
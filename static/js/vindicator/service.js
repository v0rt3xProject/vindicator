function getUpdates() {
    send({
        view: "service",
        action: "list"
    })
}

function setUpdates(data) {
    if (data.view == "service") {
        if (data.type == "add") {
            if (data.success) {
                var name = $('#service_name').first();
                var port = $('#service_port').first();

                name.val("");
                port.val("");
            }
        } else if(data.type == "list") {
            for (var i = 0; i < data.service.length; i++) {
                var service = data.service[i];
                if (!$("tr#" + service.id).length) {
                    var service_row = $("<tr></tr>").attr("id", service.id);

                    service_row.append($("<td></td>").text(service.id));
                    service_row.append($("<td></td>").text(service.name));
                    service_row.append($("<td></td>").text(service.port));
                    service_row.append($("<td></td>")
                        .attr("id", "status_" + service.id)
                        .attr("class", (service.available) ? "text-success" : "text-danger")
                        .text((service.available) ? "On-line" : "Off-Line"));

                    service_row.append($("<td></td>").append(
                        $("<button></button>")
                            .attr('type', 'button')
                            .attr('class', 'btn btn-danger btn-sm')
                            .click(function () {
                                deleteService(service.id);
                            })
                            .text("Delete")
                    ));

                    $("#service_list").append(service_row);
                }
            }
        } else if (data.type == "delete") {
            if (data.success) {
                $("tr#" + data.target).remove();
            }
        }
    }
}

$('#add_service').click(function () {
    var name = $('#service_name').first();
    var port = $('#service_port').first();

    if (name.val().length == 0) {
        notify("danger", "Service name shouldn't be null");
        return;
    }

    if (port.val().length == 0) {
        notify("danger", "Servuice port shouldn't be null");
        return;
    }

    send({
        view: "service",
        action: "add",
        name: name.val(),
        port: port.val(),
    });
});


function deleteService(serviceName) {
    send({
        action: 'delete',
        view: 'service',
        target: serviceName
    });
}

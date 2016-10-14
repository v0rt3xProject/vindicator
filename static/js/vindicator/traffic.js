var service_filter = $("#service").first();
var direction_filter = $("#direction").first();
var client_filter = $("#client").first();
var length_filter = $("#length").first();
var data_filter = $("#data").first();

var sequence_filter = $("#sequence").first();
var sequence_range = $("#sequence_range").first();

var show_limit = $("#limit").first();
var show_offset = $("#offset").first();

var traffic_list = $("#traffic").first();
var traffic_data_list = $("#traffic_data").first();

function setFilter(field, value) {
    var field_element = $('#' + field).first();
    console.log("Setting '" + field + "' to '" + value + "'");
    field_element.val(value);
    field_element.trigger("change");
}

function getUpdates() {
    send({
        action: "list",
        view: "traffic",
        limit: parseInt(show_limit.val()),
        offset: parseInt(show_offset.val()),
        filter: {
            service: service_filter.val(),
            direction: direction_filter.val(),
            client: client_filter.val(),
            length: length_filter.val(),
            sequence: sequence_filter.val(),
            sequenceRange: sequence_range.val(),
            data: data_filter.val()
        }
    });
}

function setUpdates(data) {
    if (data.view == "traffic") {
        if (data.type == "list") {
            var active_ids = [];
            var packet;

            for (var i = 0; i < data.traffic.length; i++) {
                packet = data.traffic[i];
                active_ids.push(packet.id);
            }

            function isActive() {
                if ($.inArray(this.id.split("_")[1], active_ids) < 0) {
                    this.remove();
                }
            }

            traffic_list.children('tr').each(isActive);
            traffic_data_list.children('div').each(isActive);

            for (var j = 0; j < data.traffic.length; j++) {
                packet = data.traffic[j];
                if (!$('tr#packet_' + packet.id).length) {
                    var tr_created = false;

                    var tr = $('<tr></tr>').attr('id', 'packet_' + packet.id);

                    tr.append($("<td></td>").append(
                        $("<a></a>").click(function () {
                            setFilter('service', packet.service.name);
                        }).text(packet.service.name + " (" + packet.service.port + ")")
                    ));
                    tr.append($("<td></td>").append(
                        $("<a></a>").click(function () {
                            setFilter('direction', packet.direction ? "in" : "out");
                        }).text(packet.direction ? " Inbound " : " Outgoing ")
                    ));
                    tr.append($("<td></td>").append(
                        $("<a></a>").click(function () {
                            setFilter('client', packet.client);
                        }).text(packet.client)
                    ));
                    tr.append($("<td></td>").text(packet.length));
                    tr.append($("<td></td>").append(
                        $("<a></a>").click(function () {
                            setFilter('sequence', packet.sequence);
                        }).text(packet.sequence)
                    ));
                    tr.append($("<td></td>").text(packet.time));

                    tr.append($("<td></td>").append(
                        $("<button></button>")
                            .attr('type', 'button')
                            .attr('class', 'btn btn-primary btn-sm')
                            .attr('data-toggle', 'tab')
                            .attr('data-target', '#data_' + packet.id)
                            .text("Show")
                    ));

                    for (var k = $.inArray(packet.id, active_ids) - 1; k >= 0; k--) {
                        var beforeTRElement = $('tr#packet_' + active_ids[k]);
                        if (beforeTRElement.length) {
                            beforeTRElement.after(tr);
                            tr_created = true;
                            break;
                        }
                    }

                    if (!tr_created) {
                        traffic_list.prepend(tr);
                    }
                }

                if (!$('div#data_' + packet.id).length) {
                    var div_created = false;

                    var div = $('<div></div>').attr('id', 'data_' + packet.id).attr('class', 'tab-pane');

                    div.append(
                        $("<ul></ul>").attr('class', 'nav nav-tabs').append(
                            $("<li></li>").attr('class', 'active').append(
                                $("<a></a>").attr('data-toggle', 'tab').attr('href', '#data_hex_' + packet.id).text("HEX")
                            )
                        ).append(
                            $("<li></li>").append(
                                $("<a></a>").attr('data-toggle', 'tab').attr('href', '#data_ascii_' + packet.id).text("ASCII")
                            )
                        )
                    ).append(
                        $('<div></div>').attr('class', 'tab-content').append(
                            $('<div></div>').attr('class', 'tab-pane active').attr('id', 'data_hex_' + packet.id).append(
                                $('<pre></pre>').text(packet.data.hex)
                            )
                        ).append(
                            $('<div></div>').attr('class', 'tab-pane').attr('id', 'data_ascii_' + packet.id).append(
                                $('<pre></pre>').text(packet.data.ascii)
                            )
                        )
                    );

                    for (var l = $.inArray(packet.id, active_ids) - 1; l >= 0; l--) {
                        var beforeDIVElement = $('div#data_' + active_ids[l]);
                        if (beforeDIVElement.length) {
                            beforeDIVElement.after(div);
                            div_created = true;
                            break;
                        }
                    }

                    if (!div_created) {
                        traffic_data_list.prepend(div);
                    }
                }
            }
        }
    }
}

service_filter.on("change", getUpdates());
direction_filter.on("change", getUpdates());
client_filter.on("change", getUpdates());
length_filter.on("change", getUpdates());
data_filter.on("change", getUpdates());

show_limit.on("change", getUpdates());
show_offset.on("change", getUpdates());
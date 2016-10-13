var socket, refreshIntervalId;
var ready = false;

function errorToString(error) {
    if (error.code == 1006) {
        return "Connection to server lost!";
    } else {
        return "Error[" + error.code + "]: " + error.message;
    }
}

function showError(error) {
    notify("danger", '<b>Vindicator Error</b>: ' + errorToString(error))
}

function notify(type, message) {
    $.notify({
        icon: "notifications",
        message: message
    }, {
        type: type,
        timer: 3000
    });
}

function checkUpdates() {
    if (ready) {
        if (typeof getUpdates === "function") {
            getUpdates();
        }
    }
}

function handleMessage(event) {
    var data = JSON.parse(event.data);

    if (data.notify) {
        notify((data.success) ? "success" : "danger", data.message);
    }

    if (data.view == "flag" && data.type == "submit") {
        if (data.success) {
            $("#flag_to_submit").val("");
        }
    } else {
        if (typeof setUpdates === "function") {
            setUpdates(data);
        }
    }
}

function send(data) {
    socket.send(JSON.stringify(data));
}

function init(ws_path) {
    socket = new WebSocket(ws_path);

    socket.onmessage = handleMessage;

    socket.onopen = function() {
        console.log("Connection to Vindicator established");
        ready = true;
    };

    socket.onclose = function(event){
        ready = false;
        showError(event);

        clearInterval(refreshIntervalId);

        setTimeout(function(){
            init(ws_path);
        }, 7000);
    };

    socket.onerror = function(error) {
        showError(error);
    };

    refreshIntervalId = setInterval(checkUpdates, 2000);
}

function submitFlag() {
    send({
        view: "flag",
        action: "submit",
        flag: $("#flag_to_submit").val()
    });
}

$("#submit_flag").click(submitFlag);


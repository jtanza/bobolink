const http = function() {
    this.request = function (url, data, success, error, verb) {
        const request = new XMLHttpRequest();
        request.onreadystatechange = function () {
            if (request.readyState === 4) {
                if (request.status === 200) {
                    success(request.responseText);
                } else {
                    error(request);
                }
            }
        };

        request.open(verb, url);
        request.setRequestHeader("Content-Type", "application/json;charset=UTF-8",)
        request.setRequestHeader("Accept", "text/html")
        request.send(data);
    }

    this.post = function(url, data, success, error) {
        this.request(url, data, success, error, "POST")
    };

    this.put = function(url, data, success, error) {
        this.request(url, data, success, error, "PUT")
    };

}; const client = new http();

function search() {
    const query = document.getElementById("resource-search").value
    const path = (query === ".*" || query === "*") ? "links/all" : "links/find"
    client.post(path, JSON.stringify({"query": query}), function(resp) {
        document.getElementById("search-results").innerHTML = resp ? resp : "No Matches.";
    }, function (resp) {
        document.getElementById("search-results").innerText = resp.responseText
    });
}

function add() {
    const urls = document.getElementById("add").value;
    client.put("links/add", JSON.stringify({"urls": urls.trim().split(" ")}), function(resp) {
        document.getElementById("add-form").style.display = "none"
        document.getElementById("added-title").innerText = "Successfully Added:"
        document.getElementById("added-results").innerHTML = resp;
    }, function (resp) {
        document.getElementById("add-form").style.display = "none"
        document.getElementById("added-title").innerText = "Error:"
        document.getElementById("added-results").innerHTML = resp.responseText;
    });
}

function del() {
    const boxes = document.getElementsByName("delete-box");
    let selected = []
    for (let i = 0; i < boxes.length; i++) {
        if (boxes[i].checked) {
            selected.push(boxes[i].value);
        }
    }

    if (selected.length > 0) {
        if (confirm("Are you sure you'd like to delete the " + selected.length + " url/s selected?\nThis action cannot be undone.")) {
            client.put("links/remove", JSON.stringify({"urls": selected}), function(resp) {
                document.getElementById("delete-list").style.display = "none"
                document.getElementById("deleted-title").innerText = "Successfully Deleted:"
                document.getElementById("deleted-results").innerHTML = resp;
            }, function (resp) {
                document.getElementById("delete-list").style.display = "none"
                document.getElementById("deleted-title").innerText = "Error:"
                document.getElementById("deleted-results").innerHTML = resp.responseText;
            });
        }
    }
}

function toggle(source) {
    const checkboxes = document.getElementsByName("delete-box");
    for(let i = 0; i < checkboxes.length; i++) {
        checkboxes[i].checked = source.checked;
    }
}
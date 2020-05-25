const http = function () {
    this.request = function (url, data, headers, success, error, verb) {
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

        Object.keys(headers).forEach(function (k) {
            request.setRequestHeader(k, headers[k])
        });

        if (data) {
            request.send(data);
        } else {
            request.send();
        }
    }

    this.get = function(url, success, error) {
        this.request(url, null, {}, success, error, "GET")
    };
    this.postWithHeaders = function(url, data, headers, success, error) {
        this.request(url, data, headers, success, error, "POST")
    };
    this.putWithHeaders = function(url, data, headers, success, error) {
        this.request(url, data, headers, success, error, "POST")
    };
    this.post = function(url, data, success, error) {
        this.request(url, data, {"Content-Type":"application/json;charset=UTF-8"}, success, error, "POST")
    };
    this.put = function(url, data, success, error) {
        this.request(url, data, {"Content-Type":"application/json;charset=UTF-8"}, success, error, "PUT")
    };

}; const client = new http();

function search() {
    const headers = {
        "Content-Type":"application/json;charset=UTF-8",
        "Accept": "text/html"
    }
    const query = document.getElementById("resource-search").value
    const path = (query === ".*" || query === "*") ? "links/all" : "links/find"
    client.postWithHeaders(path, JSON.stringify({"query": query}), headers, function(resp) {
        document.getElementById("search-results").innerHTML = resp ? resp : "No Matches.";
    }, function (resp) {
        document.getElementById("search-results").innerText = resp.responseText
    });
}

function add() {
    const headers = {
        "Content-Type":"application/json;charset=UTF-8",
        "Accept": "text/html"
    }
    const urls = document.getElementById("add").value;
    client.putWithHeaders("links/add", JSON.stringify({"urls": urls.trim().split(" ")}), headers, function(resp) {
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
            const headers = {
                "Content-Type":"application/json;charset=UTF-8",
                "Accept": "text/html"
            }
            client.putWithHeaders("links/remove", JSON.stringify({"urls": selected}), headers, function(resp) {
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
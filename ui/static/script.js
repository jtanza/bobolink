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
    const path = query === ".*" ? "links/all" : "links/find"
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
    const urls = document.getElementById("manage").value;
    client.putWithHeaders("links/add", JSON.stringify({"urls": urls.trim().split(" ")}), headers, function(resp) {
        document.getElementById("add-form").style.display = "none"
        document.getElementById("added-title").innerText = "Successfully Added:"
        document.getElementById("manage-results").innerHTML = resp;
    }, function (resp) {
        document.getElementById("add-form").style.display = "none"
        document.getElementById("added-title").innerText = "Error:"
        document.getElementById("manage-results").innerHTML = resp.responseText;
    });
}




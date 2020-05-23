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
    const query = {"query": document.getElementById("resource-search").value}
    client.postWithHeaders("links/find", JSON.stringify(query), headers, function(resp) {
        // TODO handle empty search
        document.getElementById("search-results").innerHTML = resp;
    }, function (resp) {
        console.log(resp); // TODO
    });
}

function add() {
    const headers = {
        "Content-Type":"application/json;charset=UTF-8",
        "Accept": "text/html"
    }
    const textarea = document.getElementById("manage").value;
    const urls =  {"urls": textarea.split(" ")}
    client.putWithHeaders("links/add", JSON.stringify(urls), headers, function(resp) {
        // TODO handle empty search
        document.getElementById("add-form").style.display = "none"
        document.getElementById("added-title").style.display = "block"
        document.getElementById("manage-results").innerHTML = resp;
    }, function (resp) {
        console.log(resp); // TODO
    });
}




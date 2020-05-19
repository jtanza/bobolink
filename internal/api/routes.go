package api

// TODO possibly move this back to server command

import (
	"encoding/json"
	"fmt"
	"github.com/jtanza/bobolink/internal/search"
	"io"
	"log"
	"net/http"
)

type URLs struct {
	URLs []string `json:"urls"`
}

func Add(w http.ResponseWriter, r *http.Request) {
	var u URLs
	unmarshall(r.Body, &u)
	search.AddResources(u.URLs)
	// w.Write() 200 Ok
}

func Find(w http.ResponseWriter, r *http.Request) {
	query := ""
	unmarshall(r.Body, query)
	matches := search.Query(query)
	fmt.Println(matches)
	// w.Write(matches) 200 Ok
}

func All(w http.ResponseWriter, r *http.Request) {
	matches := search.MatchAll()
	fmt.Println(matches)
	// w.Write(matches) 200 Ok
}

func unmarshall(r io.ReadCloser, v interface{}) {
	dec := json.NewDecoder(r)
	if err := dec.Decode(v); err != nil {
		log.Fatal(err)
	}
}

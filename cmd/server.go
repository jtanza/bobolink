package cmd

import (
	"encoding/json"
	"fmt"
	"github.com/jtanza/bobolink/internal/search"
	"github.com/spf13/cobra"
	"io"
	"log"
	"net/http"
)

const (
	ContentType = "Content-Type"
	ContentTypeJSON = "application/json"
)

func init() {
	var serverCmd = &cobra.Command{
		Use:   "server",
		Short: "starts the bobolink web server",
		Run: func(cmd *cobra.Command, args []string) {
			http.HandleFunc("/links/add", add)
			http.HandleFunc("/links/find", find)
			http.HandleFunc("/links/all", all)
			http.HandleFunc("/links/remove", remove)
			http.HandleFunc("/", page)

			fmt.Println("Listening on port 8080...")
			log.Fatal(http.ListenAndServe(":8080", nil)) // TODO
		},
	}
	rootCmd.AddCommand(serverCmd)
}
type URLs struct {
	URLs []string `json:"urls"`
}

func add(w http.ResponseWriter, r *http.Request) {
	var u URLs
	unmarshall(r.Body, &u)
	if err := search.AddResources(u.URLs); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusBadRequest)
	}
}

func find(w http.ResponseWriter, r *http.Request) {
	query := struct {
		Query string `json:"query"`
	}{}
	unmarshall(r.Body, &query)
	matches := search.Query(query.Query)

	if err := marshall(w, matches); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func all(w http.ResponseWriter, r *http.Request) {
	matches := search.MatchAll()
	resp := make([]string, 0, len(matches))
	for _, m := range matches {
		resp = append(resp, m.URL)
	}

	if err := marshall(w, resp); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func remove(w http.ResponseWriter, r *http.Request) {
	var u URLs
	unmarshall(r.Body, &u)
	if err := search.Delete(u.URLs); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func page(w http.ResponseWriter, r *http.Request) {
	http.ServeFile(w, r, "ui/html/index.html")
	/*
	tmpl := template.Must(template.ParseFiles("ui/html/index.html"))
	tmpl.Execute(w, nil)
	 */
}

func unmarshall(r io.ReadCloser, v interface{}) {
	dec := json.NewDecoder(r)
	if err := dec.Decode(v); err != nil {
		log.Fatal(err)
	}
}

func marshall(w http.ResponseWriter, v interface{}) error {
	w.Header().Set(ContentType, ContentTypeJSON)
	w.WriteHeader(http.StatusOK) // TODO
	return json.NewEncoder(w).Encode(v)
}

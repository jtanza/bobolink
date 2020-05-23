package cmd

import (
	"encoding/json"
	"fmt"
	"github.com/jtanza/bobolink/internal/search"
	"github.com/spf13/cobra"
	"html/template"
	"io"
	"log"
	"net/http"
	"net/url"
)

const (
	Accept = "Accept"
	ContentType = "Content-Type"
	ContentTypeJSON = "application/json"
	ContentTypeHTML = "text/html"
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
			http.HandleFunc("/manage", manage)
			http.HandleFunc("/", index)

			http.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("ui/static/"))))

			fmt.Println("Listening on port 8080...")
			log.Fatal(http.ListenAndServe(":8080", nil)) // TODO
		},
	}
	rootCmd.AddCommand(serverCmd)
}
type URLs struct {
	URLs []string `json:"urls"`
}
type SearchQuery struct {
	Query string `json:"query"`
}
type DocumentWithHost struct {
	Document search.Document
	Host string
}

func add(w http.ResponseWriter, r *http.Request) {
	var u URLs
	unmarshall(r.Body, &u)

	added, err := search.AddResources(u.URLs)
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusBadRequest)
	}

	toResponse(w, r, added, toDocumentWithHosts(added), "ui/html/urls.html")
}

func find(w http.ResponseWriter, r *http.Request) {
	var q SearchQuery
	unmarshall(r.Body, &q)
	matches := search.Query(q.Query)

	toResponse(w, r, matches, toDocumentWithHosts(matches), "ui/html/documents.html")
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

func index(w http.ResponseWriter, r *http.Request) {
	files := []string{
		"ui/html/search.html",
		"ui/html/base.html",
	}
	tmpl := template.Must(template.ParseFiles(files...))
	if err := tmpl.Execute(w, nil); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func manage(w http.ResponseWriter, r *http.Request) {
	files := []string{
		"ui/html/manage.html",
		"ui/html/base.html",
	}
	tmpl := template.Must(template.ParseFiles(files...))
	if err := tmpl.Execute(w, nil); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func formatHost(u string) string {
	p, err := url.Parse(u)
	if err != nil {
		log.Fatal(err)
	}
	return p.Host
}

func unmarshall(r io.ReadCloser, schema interface{}) {
	dec := json.NewDecoder(r)
	if err := dec.Decode(schema); err != nil {
		log.Fatal(err)
	}
}

func marshall(w http.ResponseWriter, v interface{}) error {
	w.Header().Set(ContentType, ContentTypeJSON)
	w.WriteHeader(http.StatusOK) // TODO
	return json.NewEncoder(w).Encode(v)
}

func renderTemplate(w http.ResponseWriter, data interface{}, files...string) error {
	tmpl := template.Must(template.ParseFiles(files...))
	w.Header().Set(ContentType, ContentTypeHTML)
	return tmpl.Execute(w, data)
}

func toDocumentWithHosts(d []search.Document) []DocumentWithHost {
	resp := make([]DocumentWithHost, 0, len(d))
	for _, m := range d {
		resp = append(resp, DocumentWithHost{
			Document: m,
			Host:   formatHost(m.URL),
		})
	}
	return resp
}

// TODO have this take a function that transforms the json to html in the case block
func toResponse(w http.ResponseWriter, r *http.Request, json interface{}, html interface{}, path string) {
	switch r.Header.Get(Accept) {
	case ContentTypeHTML:
		if err := renderTemplate(w, html, path); err != nil {
			http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
		}
	default:
		if err := marshall(w, json); err != nil {
			http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
		}
	}
}
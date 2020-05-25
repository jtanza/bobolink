package cmd

import (
	"encoding/json"
	"fmt"
	"github.com/blevesearch/bleve/analysis/char/html"
	"github.com/jtanza/bobolink/internal"
	"github.com/spf13/cobra"
	"html/template"
	"io"
	"log"
	"net/http"
	"net/url"
	"sort"
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
			http.HandleFunc("/", indexView)
			http.HandleFunc("/add", addView)
			http.HandleFunc("/delete", deleteView)

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
type TemplateDocument struct {
	Body template.HTML
	URL string
	Host string
}

func add(w http.ResponseWriter, r *http.Request) {
	var u URLs
	unmarshall(r.Body, &u)

	added, err := internal.AddResources(u.URLs)
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusBadRequest)
	}

	toResponse(w, r, added, toTemplateDocuments(added), "ui/html/urls.html")
}

func find(w http.ResponseWriter, r *http.Request) {
	var q SearchQuery
	unmarshall(r.Body, &q)

	matches, err := internal.QueryWithHighlight(q.Query, html.Name)
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusBadRequest)
	}

	toResponse(w, r, matches, toTemplateDocuments(matches), "ui/html/documents.html")
}

func all(w http.ResponseWriter, r *http.Request) {
	matches, err := internal.MatchAll()
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusBadRequest)
	}

	urls := make([]string, 0, len(matches))
	for _, m := range matches {
		urls = append(urls, m.URL)
	}

	toResponse(w, r, urls, toTemplateDocuments(matches), "ui/html/urls.html")
}

func remove(w http.ResponseWriter, r *http.Request) {
	var u URLs
	unmarshall(r.Body, &u)

	deleted, err := internal.Delete(u.URLs)
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
	toResponse(w, r, deleted, urlsToTemplateDocs(deleted), "ui/html/urls.html")
}

func indexView(w http.ResponseWriter, r *http.Request) {
	files := []string{
		"ui/html/search.html",
		"ui/html/base.html",
	}
	tmpl := template.Must(template.ParseFiles(files...))
	if err := tmpl.Execute(w, nil); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func addView(w http.ResponseWriter, r *http.Request) {
	files := []string{
		"ui/html/add.html",
		"ui/html/base.html",
	}
	tmpl := template.Must(template.ParseFiles(files...))
	if err := tmpl.Execute(w, nil); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func deleteView(w http.ResponseWriter, r *http.Request) {
	files := []string{
		"ui/html/delete.html",
		"ui/html/base.html",
	}

	docs, err := internal.MatchAll()
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
	sort.SliceStable(docs, func(i, j int) bool {
		return docs[i].URL < docs[j].URL
	})

	tmpl := template.Must(template.ParseFiles(files...))
	if err := tmpl.Execute(w, toTemplateDocuments(docs)); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func toURL(u string) *url.URL {
	p, err := url.Parse(u)
	if err != nil {
		log.Fatal(err)
	}
	return p
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

func toTemplateDocuments(d []internal.Document) []TemplateDocument {
	resp := make([]TemplateDocument, 0, len(d))
	for _, m := range d {
		resp = append(resp, TemplateDocument{
			Body: template.HTML(m.EscapeBody()),
			URL: m.URL,
			Host: toURL(m.URL).Host,
		})
	}
	return resp
}

func urlsToTemplateDocs(urls []string) []TemplateDocument {
	resp := make([]TemplateDocument, 0, len(urls))
	for _, u := range urls {
		parsed := toURL(u)
		resp = append(resp, TemplateDocument{
			URL: parsed.String(),
			Host: parsed.Host,
		})
	}
	return resp
}

// TODO have this take a function that transforms the json to html in the case block
func toResponse(w http.ResponseWriter, r *http.Request, j interface{}, h interface{}, path string) {
	switch r.Header.Get(Accept) {
	case ContentTypeHTML:
		if err := renderTemplate(w, h, path); err != nil {
			http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
		}
	default:
		if err := marshall(w, j); err != nil {
			http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
		}
	}
}
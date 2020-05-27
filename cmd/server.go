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
	"strings"
)

// Set of internally used headers and header values
const (
	Accept          = "Accept"
	ContentType     = "Content-Type"
	ContentTypeJSON = "application/json"
	ContentTypeHTML = "text/html"
)

var (
	search *internal.Search
	port   string
)

func init() {
	var server = &cobra.Command{
		Use:   "server",
		Short: "Starts the bobolink web server",
		Run: func(cmd *cobra.Command, args []string) {
			search = internal.NewSearch(indexPath)
			http.HandleFunc("/links/add", add)
			http.HandleFunc("/links/find", find)
			http.HandleFunc("/links/all", all)
			http.HandleFunc("/links/remove", remove)
			http.HandleFunc("/", indexView)
			http.HandleFunc("/add", addView)
			http.HandleFunc("/delete", deleteView)

			http.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("ui/static/"))))

			if !strings.HasPrefix(port, ":") {
				port = ":" + port
			}
			fmt.Printf("Listening on port %s...\n", port)

			log.Fatal(http.ListenAndServe(port, nil))
		},
	}
	server.Flags().StringVarP(&port, "port", "p", ":8080", "set port")
	root.AddCommand(server)
}

type urls struct {
	URLs []string `json:"urls"`
}

func add(w http.ResponseWriter, r *http.Request) {
	var u urls
	unmarshall(r.Body, &u)

	added, err := search.AddResources(u.URLs)
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusBadRequest)
	}

	toResponse(w, r, added, docsToTemplateDocs(added), "ui/html/urls.html")
}

func find(w http.ResponseWriter, r *http.Request) {
	q := struct {
		Query string `json:"query"`
	}{}
	unmarshall(r.Body, &q)

	matches, err := search.QueryWithHighlight(q.Query, html.Name)
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusBadRequest)
	}

	toResponse(w, r, matches, docsToTemplateDocs(matches), "ui/html/documents.html")
}

func all(w http.ResponseWriter, r *http.Request) {
	matches, err := search.MatchAll()
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusBadRequest)
	}

	urls := make([]string, 0, len(matches))
	for _, m := range matches {
		urls = append(urls, m.URL)
	}

	toResponse(w, r, urls, docsToTemplateDocs(matches), "ui/html/urls.html")
}

func remove(w http.ResponseWriter, r *http.Request) {
	var u urls
	unmarshall(r.Body, &u)

	deleted, err := search.Delete(u.URLs)
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
	toResponse(w, r, deleted, urlsToTemplateDocs(deleted), "ui/html/urls.html")
}

func indexView(w http.ResponseWriter, r *http.Request) {
	tmpl := buildStaticTemplate(w, []string{"ui/html/search.html", "ui/html/base.html"}...)
	if err := tmpl.Execute(w, nil); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func addView(w http.ResponseWriter, r *http.Request) {
	tmpl := buildStaticTemplate(w, []string{"ui/html/add.html", "ui/html/base.html"}...)
	if err := tmpl.Execute(w, nil); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
}

func deleteView(w http.ResponseWriter, r *http.Request) {
	docs, err := search.MatchAll()
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
	sort.SliceStable(docs, func(i, j int) bool {
		return docs[i].URL < docs[j].URL
	})

	tmpl := buildStaticTemplate(w, []string{"ui/html/delete.html", "ui/html/base.html"}...)
	if err := tmpl.Execute(w, docsToTemplateDocs(docs)); err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
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

func buildStaticTemplate(w http.ResponseWriter, file ...string) *template.Template {
	b := make([]byte, 0)
	for _, f := range file {
		d, err := internal.Asset(f)
		if err != nil {
			http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
		}
		b = append(b, d...)
	}
	temp, err := template.New("").Parse(string(b))
	if err != nil {
		http.Error(w, fmt.Sprint(err), http.StatusInternalServerError)
	}
	return temp
}

func renderTemplate(w http.ResponseWriter, data interface{}, file string) error {
	tmpl := buildStaticTemplate(w, file)
	w.Header().Set(ContentType, ContentTypeHTML)
	return tmpl.Execute(w, data)
}

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

func toURL(u string) *url.URL {
	p, err := url.Parse(u)
	if err != nil {
		log.Fatal(err)
	}
	return p
}

type templateDocument struct {
	Body template.HTML
	URL  string
	Host string
}

func docsToTemplateDocs(d []internal.Document) []templateDocument {
	resp := make([]templateDocument, 0, len(d))
	for _, m := range d {
		resp = append(resp, templateDocument{
			Body: template.HTML(m.EscapeBody()),
			URL:  m.URL,
			Host: toURL(m.URL).Host,
		})
	}
	return resp
}

func urlsToTemplateDocs(urls []string) []templateDocument {
	resp := make([]templateDocument, 0, len(urls))
	for _, u := range urls {
		parsed := toURL(u)
		resp = append(resp, templateDocument{
			URL:  parsed.String(),
			Host: parsed.Host,
		})
	}
	return resp
}

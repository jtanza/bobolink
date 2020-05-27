package internal

import (
	"fmt"
	"github.com/blevesearch/bleve"
	_ "github.com/blevesearch/bleve/search/highlight/highlighter/ansi"
	_ "github.com/blevesearch/bleve/search/highlight/highlighter/html"
	"log"
	"os"
	"strings"
)

const indexFile = "index.bleve"
const indexEnv = "BOBOLINK_DIR"

type Search struct {
	IndexPath string
	index bleve.Index
}

func NewSearch(indexPath string) *Search {
	return &Search{
		IndexPath: indexPath,
		index: openIndex(indexPath),
	}
}

func (s Search) AddResources(resources []string) ([]Document, error) {
	docs, err := Convert(resources)
	if err != nil {
		return nil, err
	}

	b := s.index.NewBatch()
	for _, d := range docs {
		if err := b.Index(d.Id, d); err != nil {
			log.Fatal(err)
		}
	}
	if err := s.index.Batch(b); err != nil {
		return nil, err
	}

	return docs, nil
}

func (s Search) Query(q string) ([]Document, error) {
	return s.QueryWithHighlight(q, "")
}

func (s Search) QueryWithHighlight(q string, highlight string) ([]Document, error)  {
	mq := bleve.NewQueryStringQuery(q)
	r := bleve.NewSearchRequest(mq)

	if highlight != "" {
		r.Highlight = bleve.NewHighlightWithStyle(highlight)
	} else {
		r.Highlight = bleve.NewHighlight()
	}
	r.Highlight.AddField(Body)

	r.Fields = append(r.Fields, Body)
	r.Fields = append(r.Fields, URL)

	return search(r, s.index)
}

func (s Search) MatchAll() ([]Document, error) {
	q := bleve.NewMatchAllQuery()
	r := bleve.NewSearchRequest(q)
	r.Fields = append(r.Fields, URL)

	return search(r, s.index)
}

func (s Search) Delete(urls []string) ([]string, error) {
	b := s.index.NewBatch()
	for _, u := range urls {
		b.Delete(u)
	}
	if err := s.index.Batch(b); err != nil {
		return nil, err
	}
	return urls, nil
}

func search(r *bleve.SearchRequest, i bleve.Index) ([]Document, error) {
	search, err := i.Search(r)
	if err != nil {
		return nil, err
	}

	res := make([]Document, 0, search.Total)
	if search.Total > 0 {
		for _, h := range search.Hits {
			doc := Document{
				Id:   fmt.Sprint(h.Fields[Id]),
				URL:  fmt.Sprint(h.Fields[URL]),
				Body: fmt.Sprint(strings.Join(h.Fragments[Body], " ")),
			}
			res = append(res, doc)
		}
	}
	return res, nil
}

func openIndex(path string) bleve.Index {
	if path == "" {
		env, ok := os.LookupEnv(indexEnv)
		if !ok {
			log.Fatalf("Cannot find index. Please set env var %s or pass index dir explicitly with flag: --index-path", indexEnv)
		}
		path = env
	}

	if !strings.HasSuffix(path, "/") {
		path += "/"
	}
	path += indexFile

	var idx bleve.Index
	if exists(path) {
		openIdx, err := bleve.Open(path)
		if err != nil {
			log.Fatal(err)
		}
		idx = openIdx
	} else {
		newIdx, err := bleve.New(path, bleve.NewIndexMapping())
		if err != nil {
			log.Fatal(err)
		}
		idx = newIdx
	}
	return idx
}

func exists(path string) bool {
	if _, err := os.Stat(path); err != nil {
		return os.IsExist(err)
	}
	return true
}
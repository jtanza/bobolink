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

const indexPath = "/opt/bobolink/index.bleve" // TODO

var index = getIndexAt(indexPath)

func AddResources(resources []string) ([]Document, error) {
	docs, err := Convert(resources)
	if err != nil {
		return nil, err
	}

	b := index.NewBatch()
	for _, d := range docs {
		if err := b.Index(d.Id, d); err != nil {
			log.Fatal(err)
		}
	}
	if err := index.Batch(b); err != nil {
		return nil, err
	}

	return docs, nil
}

func Query(q string) ([]Document, error) {
	return QueryWithHighlight(q, "")
}

func QueryWithHighlight(q string, highlight string) ([]Document, error)  {
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

	return search(r)
}

func MatchAll() ([]Document, error) {
	q := bleve.NewMatchAllQuery()
	r := bleve.NewSearchRequest(q)
	r.Fields = append(r.Fields, URL)

	return search(r)
}

func Delete(urls []string) error {
	b := index.NewBatch()
	for _, u := range urls {
		b.Delete(u)
	}
	return index.Batch(b)
}

func search(r *bleve.SearchRequest) ([]Document, error) {
	search, err := index.Search(r)
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

func getIndexAt(path string) bleve.Index {
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

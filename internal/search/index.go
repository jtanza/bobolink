package search

import (
	"fmt"
	"github.com/blevesearch/bleve"
	"log"
	"os"
	"strings"
)

const indexPath = "/tmp/search.bleve" // TODO

var index = getIndexAt(indexPath) // TODO Close()?

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

func Query(q string) []Document {
	mq := bleve.NewMatchQuery(q) // TODO Regex?
	r := bleve.NewSearchRequest(mq)

	r.Highlight = bleve.NewHighlight() // TODO add ansi highlight
	r.Highlight.AddField(Body)

	r.Fields = append(r.Fields, Body)
	r.Fields = append(r.Fields, URL)

	return query(r)
}

func MatchAll() []Document {
	q := bleve.NewMatchAllQuery()
	r := bleve.NewSearchRequest(q)
	r.Fields = append(r.Fields, URL)

	return query(r)
}

func Delete(resources []string) error {
	b := index.NewBatch()
	for _, url := range resources {
		b.Delete(url)
	}
	return index.Batch(b)
}

func query(r *bleve.SearchRequest) []Document {
	search, err := index.Search(r)
	if err != nil {
		log.Fatal(err)
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
	return res
}

func getIndexAt(indexPath string) bleve.Index {
	var index bleve.Index
	if exists(indexPath) {
		openIdx, err := bleve.Open(indexPath)
		if err != nil {
			log.Fatal(err)
		}
		index = openIdx
	} else {
		newIdx, err := bleve.New(indexPath, bleve.NewIndexMapping())
		if err != nil {
			log.Fatal(err)
		}
		index = newIdx
	}
	return index
}

func exists(path string) bool {
	if _, err := os.Stat(path); err != nil {
		return os.IsExist(err)
	}
	return true
}

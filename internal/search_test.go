package internal

import (
	"github.com/blevesearch/bleve"
	"github.com/blevesearch/bleve/analysis/char/html"
	"os"
	"testing"
)

func TestQueryWithHighlight(t *testing.T) {
	index, err := bleve.New("tmpidx", bleve.NewIndexMapping())
	if err != nil {
		t.Fatal(err)
	}
	defer func() {
		if err := os.RemoveAll("tmpidx"); err != nil {
			t.Fatal(err)
		}
	}()

	u := "www.foo.com/bar"
	if err := index.Index(u, Document{
		ID:   u,
		Body: "It can be used to encapsulate a known safe fragment of HTML",
		URL:  u,
	}); err != nil {
		t.Fatal(err)
	}

	s := Search{
		index: index,
	}
	docs, err := s.QueryWithHighlight("encapsulate", html.Name)
	if err != nil {
		t.Fatal(err)
	}

	if len(docs) != 1 {
		t.Fatalf("expected 1 hit, got %d", len(docs))
	}

	compareStrings("It can be used to <mark>encapsulate</mark> a known safe fragment of HTML", docs[0].Body, t)
}

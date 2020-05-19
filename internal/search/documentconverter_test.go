package search

import (
	"fmt"
	"net/url"
	"testing"
)

func TestToDocument(t *testing.T) {
	page := []byte("<html>\n<head>\n<title>\nA Simple HTML Document\n</title>\n</head>\n<body>\n<p>This is a very simple HTML document</p>\n<p>It only has two paragraphs</p>\n</body>\n</html>")
	u, err := url.Parse("http://my-page.com")
	if err != nil {
		t.Error(err)
	}
	doc := ToDocument(page, *u)

	fmt.Println(doc)
}
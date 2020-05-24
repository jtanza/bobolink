package internal

import (
	"bytes"
	"fmt"
	"golang.org/x/net/html"
	"io/ioutil"
	"log"
	"net/http"
	"net/url"
	"strings"
)

const (
	Id = "Id"
	Body = "Body"
	URL = "URL"
)

type Document struct {
	Id string
	Body string
	URL string
}

func (d Document) String() string {
	return fmt.Sprintf("URL: %v\nMatch: %v\n", d.URL, d.Body)
}

func (d Document) EscapeBody() string {
	s := strings.ReplaceAll(html.EscapeString(d.Body), "&lt;mark&gt;", "<mark>")
	return strings.ReplaceAll(s, "&lt;/mark&gt", "</mark>")

}

func Convert(resources []string) ([]Document, error) {
	urls, err := toURLS(resources)
	if err != nil {
		return nil, err
	}

	docs := make([]Document, 0, len(resources))
	for _, u := range urls {
		body, err := download(u)
		if err != nil {
			return nil, err
		}
		docs = append(docs, ToDocument(body, u))
	}

	return docs, nil
}

func ToDocument(body []byte, u url.URL) Document {
	return Document{
		Id:   u.String(),
		Body: extractText(body),
		URL:  u.String(),
	}
}

func toURLS(urls []string) ([]url.URL, error) {
	res := make([]url.URL, 0, len(urls))
	for _, val := range urls {
		v, err := url.Parse(val) // FIXME doesnt actually validate urls
		if err != nil {
			return nil, err
		}
		res = append(res, *v)
	}
	return res, nil
}

func download(u url.URL) ([]byte, error) {
	resp, err := http.Get(u.String())
	if err != nil {
		return nil, err
	}

	defer func() {
		if err := resp.Body.Close(); err != nil {
			log.Fatal(err)
		}
	}()

	return ioutil.ReadAll(resp.Body)
}

func extractText(body []byte) string {
	tz := html.NewTokenizer(bytes.NewReader(body))
	text := make([]byte, 0)

	for {
		t := tz.Next()
		if t == html.ErrorToken {
			break
		}

		if shouldSkip(tz, t) {
			t = tz.Next()
			continue
		}
		text = append(text, tz.Text()...)
	}
	return strings.Join(strings.Fields(string(text)), " ")
}

func shouldSkip(tz *html.Tokenizer, t html.TokenType) bool {
	tag, _ := tz.TagName()
	return bytes.Compare(tag, []byte("script")) == 0 || t == html.CommentToken || t == html.DoctypeToken
}
package search

import (
	"bytes"
	"golang.org/x/net/html"
	"io/ioutil"
	"net/http"
	"net/url"
)

type Document struct {
	Id string
	Body string
	URL string
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
		Body: string(extractText(body)),
		URL: u.String(),
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
	defer resp.Body.Close()

	return ioutil.ReadAll(resp.Body)
}

func extractText(body []byte) []byte {
	tok := html.NewTokenizer(bytes.NewReader(body))
	res := make([]byte, 0)
	for {
		t := tok.Next()
		if t == html.ErrorToken {
			return res
		}
		res = append(res, tok.Text()...)
	}
}
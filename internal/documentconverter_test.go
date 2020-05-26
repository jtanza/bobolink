package internal

import (
	"net/url"
	"testing"
)

func TestToDocument(t *testing.T) {
	page := []byte("<html>\n<head>\n<title>\nA Simple HTML Document\n</title>\n</head>\n<body>\n<p>This is a very simple HTML document</p>\n<p>It only has two paragraphs</p>\n</body>\n</html>")
	u, err := url.Parse("http://my-page.com")
	if err != nil {
		t.Error(err)
	}

	got := makeDocument(page, *u)
	compareStrings(got.URL, "http://my-page.com", t)
	compareStrings(got.Body, "A Simple HTML Document This is a very simple HTML document It only has two paragraphs", t)
}

func TestToDocument_allTags(t *testing.T) {
	page := []byte("<p>Links:</p><ul><li><a href=\"foo\">Foo</a><li>\n<a href=\"/bar/baz\">BarBaz</a></ul><span>TEXT <b>I</b> NEED\n IT</span>\n<script type='text/javascript'>\n/* <![CDATA[ */\nvar post_notif_widget_ajax_obj = {\"ajax_url\":\"http:\\/\\/site.com\\/wp-admin\\/admin-ajax.php\",\"nonce\":\"9b8270e2ef\",\"processing_msg\":\"Processing...\"};\n/* ]]> */\n</script>`")
	u, err := url.Parse("http://a-page.com")
	if err != nil {
		t.Error(err)
	}

	compareStrings(makeDocument(page, *u).Body, "Links:Foo BarBazTEXT I NEED IT", t)
}

func TestToDocument_comments(t *testing.T) {
	page := []byte("<!DOCTYPE html>\n<html class=\"client-nojs\"lang=\"en\" dir=\"ltr\">\n<head>\n<meta charset=\"UTF-8\"/>\n<title>Compare-and-swap - Wikipedia</title><!--[if lt IE 9]><script src=\"/w/resources/lib/html5shiv/html5shiv.js\"></script><![endif]--></head>")
	u, err := url.Parse("http://a-page.com")
	if err != nil {
		t.Error(err)
	}

	compareStrings(makeDocument(page, *u).Body, "Compare-and-swap - Wikipedia", t)
}

func TestDocument_EscapeBody(t *testing.T) {
	d := Document{
		Body: "Disjoint <mark>Set</mark> Union (Union Find) <iframe src=\"https://www.googletagmanager.com/ns.html?id=GTM-PBHB…</li>",
	}

	s := d.EscapeBody()
	compareStrings("Disjoint <mark>Set</mark>; Union (Union Find) &lt;iframe src=&#34;https://www.googletagmanager.com/ns.html?id=GTM-PBHB…&lt;/li&gt;", s, t)
}

func compareStrings(got string, want string, t *testing.T) {
	if got != want {
		t.Errorf("got: %s; want: %s", got, want)
	}
}
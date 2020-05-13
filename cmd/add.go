package cmd

import (
	"bytes"
	"compress/gzip"
	"crypto/sha256"
	"fmt"
	"github.com/spf13/cobra"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
)

func init() {
	var addCmd = &cobra.Command{
		Use:   "add",
		Short: "add links to store",
		Run: func(cmd *cobra.Command, args []string) {
			add(args)
		},
	}
	rootCmd.AddCommand(addCmd)
}

func add(args []string) {
	urls, err := toURLS(args)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	for _, u := range urls {
		body, err := download(u)
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
		err = write(compress(body), u)
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
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
	fmt.Printf("Downloading %s...\n", u.String())

	resp, err := http.Get(u.String())
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}
	return body, nil
}

func write(b []byte, u url.URL) error {
	p, err := makePath(u)
	if err != nil {
		return err
	}

	err = ioutil.WriteFile(p, b, 0666)
	if err != nil {
		return err
	}
	return nil
}

func makePath(u url.URL) (string, error) {
	// TODO need to make .bobolink dir
	home, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}

	h := sha256.New()
	h.Write([]byte(u.String()))

	return home + "/.bobolink/" + u.Host + fmt.Sprintf("%x", h.Sum(nil)), nil
}

func compress(b []byte) []byte {
	var buff bytes.Buffer
	zw := gzip.NewWriter(&buff) // TODO add flag for compression level

	if _, err := zw.Write(b); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	if err := zw.Close(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	return buff.Bytes()
}

package cmd

import (
	"flag"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
	"strings"
)

type AddCommand struct {
	flagSet *flag.FlagSet
	urls URLs
}

func NewAddCommand() *AddCommand {
	cmd := &AddCommand{
		flagSet: flag.NewFlagSet("add", flag.ExitOnError),
	}
	cmd.flagSet.Var(&cmd.urls, "urls", "comma separated list of urls to store")
	return cmd
}

func (cmd *AddCommand) Run() {
	for _, u := range cmd.urls {
		body, err := download(u)
		if err != nil {
			fmt.Println(err)
			continue
		}
		err = write(compress(body), u)
		if err != nil {
			fmt.Println(err)
		}
	}
}

func (cmd *AddCommand) ParseArgs(args []string) error {
	// TODO https://stackoverflow.com/questions/31786215/can-command-line-flags-in-go-be-set-to-mandatory
	return cmd.flagSet.Parse(args)
}

func (cmd *AddCommand) Name() string  {
	return cmd.flagSet.Name()
}

type URLs []url.URL

func (u *URLs) String() string {
	return fmt.Sprint(*u)
}

func (u *URLs) Set(s string) error {
	for _, val := range strings.Split(s, ",") {
		v, err := url.Parse(val) // FIXME doesnt actually validate urls
		if err != nil {
			return err
		}
		*u = append(*u, *v)
	}
	return nil
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
	// TODO use sha1 of url.String() for filename
	// TODO need to make .bobolink dir
	home, err := os.UserHomeDir()
	if err != nil {
		return err
	}
	err = ioutil.WriteFile(home + "/.bobolink/" + u.Host, b, 0666)
	if err != nil {
		return err
	}
	return nil
}

func compress(b []byte) []byte {
	return b
}

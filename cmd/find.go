package cmd

import (
	"flag"
	"fmt"
	"github.com/blevesearch/bleve/search/highlight/format/ansi"
	"github.com/jtanza/bobolink/internal"
)

type FindCommand struct {
	Search internal.Search
}

func (cmd *FindCommand) Run(args []string) error {
	f := flag.NewFlagSet("find", flag.ContinueOnError)
	all := f.Bool("all", false, "return all links")
	if err := f.Parse(args); err != nil {
		return err
	}

	if *all {
		return searchAll(cmd.Search)
	} else {
		return searchQuery(args[0], cmd.Search)
	}
}

func (cmd *FindCommand) Name() string  {
	return "find"
}

func (cmd *FindCommand) Help() string  {
	return "Search for urls"
}

func (cmd *FindCommand) Usage() string  {
	// TODO add flags
	return "bobolink find [query]"
}

func (cmd *FindCommand) String() string {
	return fmt.Sprintf("%s\n\nUsage:\n %s", cmd.Help(), cmd.Usage())
}

func searchQuery(q string, s internal.Search) error {
	docs, err := s.QueryWithHighlight(q, ansi.Name)
	if err != nil {
		return err
	}
	for _, d := range docs {
		fmt.Println(d)
	}
	return nil
}

func searchAll(s internal.Search) error {
	docs, err := s.MatchAll()
	if err != nil {
		return err
	}
	for _, d := range docs {
		fmt.Println(d.URL)
	}
	return nil
}
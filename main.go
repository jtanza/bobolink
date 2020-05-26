package main

import (
	"flag"
	"github.com/jtanza/bobolink/cmd"
	"github.com/jtanza/bobolink/internal"
	"os"
)

func main() {
	path := flag.String("index-path", "", "usage foo bar bop")
	flag.Parse()
	s := internal.NewSearch(*path)

	args := os.Args[1:]
	if hasFlag("index-path") {
		args = args[2:]
	}

	cli := cmd.NewCLI([]cmd.Command{
		&cmd.AddCommand{Search: *s},
		&cmd.FindCommand{Search: *s},
		&cmd.RemoveCommand{Search: *s},
		&cmd.ServerCommand{Search: *s},
	})
	cli.Exec(args)
}

func hasFlag(name string) bool {
	found := false
	flag.Visit(func(f *flag.Flag) {
		if f.Name == name {
			found = true
		}
	})
	return found
}
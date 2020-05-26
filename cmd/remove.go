package cmd

import (
	"github.com/jtanza/bobolink/internal"
)
type RemoveCommand struct {
	Search internal.Search
}

func (r RemoveCommand) Run(args []string) error {
	_, err := r.Search.Delete(args)
	return err
}

func (r RemoveCommand) Name() string {
	return "remove"
}

func (r RemoveCommand) Help() string {
	return "remove stored URLs"
}

func (r RemoveCommand) Usage() string {
	return "bobolink find [query]"
}
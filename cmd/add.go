package cmd

import (
	"flag"
	"fmt"
	"github.com/jtanza/bobolink/internal"
)

type AddCommand struct {
	Search internal.Search
}

func (cmd *AddCommand) Run(args []string) error {
	f := flag.NewFlagSet("add", flag.ContinueOnError)
	outputOnly := f.Bool("output-only", false, "print output only")
	if err := f.Parse(args); err != nil {
		return err
	}

	if *outputOnly {
		docs, err := internal.Convert(args[1:])
		if err != nil {
			return err
		}
		fmt.Println("Would Add:")
		for _, d := range docs {
			fmt.Printf("\nURL: %s\nText: %s\n", d.URL, d.Body)
		}
		return nil
	}

	// TODO can we just return err here?
	_, err := cmd.Search.AddResources(args)
	return err
}

func (cmd *AddCommand) Name() string {
	return "add"
}

func (cmd *AddCommand) Help() string {
	return "Add urls to store"
}

func (cmd *AddCommand) Usage() string {
	// TODO add flags
	return "bobolink add [urls]"
}

func (cmd *AddCommand) String() string {
	return fmt.Sprintf("%s\n\nUsage:\n %s", cmd.Help(), cmd.Usage())
}


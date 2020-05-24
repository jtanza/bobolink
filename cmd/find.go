package cmd

import (
	"fmt"
	"github.com/blevesearch/bleve/search/highlight/format/ansi"
	"github.com/jtanza/bobolink/internal"
	"github.com/spf13/cobra"
	"log"
)

var All bool

func init() {
	var findCmd = &cobra.Command{
		Use:   "find",
		Short: "search urls",
		Run: func(cmd *cobra.Command, args []string) {
			if All {
				findAll()
			} else {
				searchQuery(args[0])
			}
		},
	}
	findCmd.Flags().BoolVarP(&All, "all", "a", false, "return all links")
	rootCmd.AddCommand(findCmd)
}

func searchQuery(q string) {
	docs, err := internal.QueryWithHighlight(q, ansi.Name)
	if err != nil {
		log.Fatal(err)
	}
	for _, d := range docs {
		fmt.Println(d)
	}
}

func findAll() {
	docs, err := internal.MatchAll()
	if err != nil {
		log.Fatal(err)
	}
	for _, d := range docs {
		fmt.Println(d.URL)
	}
}

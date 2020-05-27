package cmd

import (
	"fmt"
	"github.com/blevesearch/bleve/search/highlight/format/ansi"
	"github.com/jtanza/bobolink/internal"
	"github.com/spf13/cobra"
	"log"
)

var returnAll bool

func init() {
	var find = &cobra.Command{
		Use:   "find",
		Short: "Searches urls",
		Long: "The find command searches the index against a provided query. " +
			"Regex search syntax is supported",
		Run: func(cmd *cobra.Command, args []string) {
			s := internal.NewSearch(indexPath)
			if returnAll {
				docs, err := s.MatchAll()
				if err != nil {
					log.Fatal(err)
				}
				for _, d := range docs {
					fmt.Println(d.URL)
				}
			} else {
				docs, err := s.QueryWithHighlight(args[0], ansi.Name)
				if err != nil {
					log.Fatal(err)
				}
				for _, d := range docs {
					fmt.Println(d)
				}
			}
		},
	}
	find.Flags().BoolVarP(&returnAll, "all", "a", false, "return all links")
	root.AddCommand(find)
}
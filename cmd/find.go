package cmd

import (
	"fmt"
	"github.com/jtanza/bobolink/internal/search"
	"github.com/spf13/cobra"
)

var All bool

func init() {
	var findCmd = &cobra.Command{
		Use:   "find",
		Short: "search urls",
		Run: func(cmd *cobra.Command, args []string) {
			if All {
				fmt.Println(findAll())
			} else {
				fmt.Println(searchQuery(args[0]))
			}
		},
	}
	findCmd.Flags().BoolVarP(&All, "all", "a", false, "return all links")
	rootCmd.AddCommand(findCmd)
}

func searchQuery(query string) []search.Document {
	return search.Query(query)
}

func findAll() []search.Document {
	return search.MatchAll()
}


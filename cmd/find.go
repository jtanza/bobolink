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
				findAll()
			} else {
				searchQuery(args[0])
			}
		},
	}
	findCmd.Flags().BoolVarP(&All, "all", "a", false, "return all links")
	rootCmd.AddCommand(findCmd)
}

func searchQuery(query string) {
	for _, d := range search.Query(query) {
		fmt.Println(d)
	}
}

func findAll() {
	for _, d := range search.MatchAll() {
		fmt.Println(d.URL)
	}
}

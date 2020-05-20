package cmd

import (
	"github.com/jtanza/bobolink/internal/search"
	"github.com/spf13/cobra"
	"log"
)

func init() {
	var addCmd = &cobra.Command{
		Use:   "remove",
		Short: "delete urls from store",
		Run: func(cmd *cobra.Command, args []string) {
			if err := search.Delete(args); err != nil {
				log.Fatal(err)
			}
		},
	}
	rootCmd.AddCommand(addCmd)
}


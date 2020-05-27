package cmd

import (
	"github.com/jtanza/bobolink/internal"
	"github.com/spf13/cobra"
	"log"
)

func init() {
	var remove = &cobra.Command{
		Use:   "remove",
		Short: "Deletes urls from store",
		Long: "The Delete command requires the exact url that was added" +
			"to the index originally",
		Run: func(cmd *cobra.Command, args []string) {
			s := internal.NewSearch(indexPath)
			if _, err := s.Delete(args); err != nil {
				log.Fatal(err)
			}
		},
	}
	root.AddCommand(remove)
}

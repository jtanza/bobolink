package cmd

import (
	"fmt"
	"github.com/jtanza/bobolink/internal"
	"github.com/spf13/cobra"
	"log"
)

var dryRun bool

func init() {
	var add = &cobra.Command{
		Use:   "add",
		Short: "Adds urls to store",
		Run: func(cmd *cobra.Command, args []string) {
			if dryRun {
				docs, err := internal.Convert(args)
				if err != nil {
					log.Fatal(err)
				}
				for _, d := range docs {
					fmt.Printf("URL: %s\nText: %s\n", d.URL, d.Body)
				}
				return
			}

			s := internal.NewSearch(indexPath)
			if _, err := s.AddResources(args); err != nil {
				log.Fatal(err)
			}
		},
	}
	add.Flags().BoolVarP(&dryRun, "dry-run", "d", false, "dry run. print what would be added to stdout")
	root.AddCommand(add)
}

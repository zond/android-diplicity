package se.oort.diplicity;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import se.oort.diplicity.apigen.SingleContainer;

public abstract class RecycleAdapter<T extends SingleContainer<?>,VH extends RecycleAdapter.ViewHolder> extends RecyclerView.Adapter<VH> {

    private List<T> items;

    public abstract class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
        public abstract void bind(T item);
    }

    public RecycleAdapter(List<T> items) {
        this.items = items;
    }

    public void Clear() {
        int before = this.items.size();
        this.items.clear();
        notifyItemRangeRemoved(0, before);
    }

    public void AddAll(List<T> games) {
        int before = this.items.size();
        this.items.addAll(games);
        notifyItemRangeInserted(before, games.size());
    }

    @Override
    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(VH holder, int position) {
        T item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
